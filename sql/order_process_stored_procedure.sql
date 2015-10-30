CREATE OR REPLACE PACKAGE ORDER_PROCESS_PACKAGE AS

-- declare only public visible procedures/functions in the specification section.

      -- version: 2.1 ( Standard version)
      PROCEDURE SAVE_ORDER_SP (l_from_address IN  address_obj,
                              l_to_address IN  address_obj,
                              l_line_items IN  Lines_table,
                              instructions IN  ORDERS.instructions%type,
                              order_id OUT ORDERS.id%type,
                              message OUT varchar2);
      
      -- Version 1(dynamic sql execution)
      PROCEDURE SAVE_ORDER_VER_1_SP (
                              from_city IN ADDRESS.city%type,
                              from_state IN ADDRESS.state%type,
                              from_zip IN ADDRESS.zip%type,
                              to_city IN ADDRESS.city%type,
                              to_state IN ADDRESS.state%type,
                              to_zip IN ADDRESS.zip%type,
                              dynamic_line_item_sql in OUT varchar2,
                              instructions IN  ORDERS.instructions%type,
                              order_id OUT ORDERS.id%type,
                              message OUT varchar2);
 
      PROCEDURE LOOKUP_ORDER_SP (l_instructions out  ORDERS.instructions%type,
                              l_line_items out  Lines_table,
                              l_from_address out  address_obj,
                              l_to_address out  address_obj,
                              l_order_id IN  ORDERS.id%type,
                              message IN out varchar2);

 
END ORDER_PROCESS_PACKAGE;
/
show errors;
/

CREATE OR REPLACE PACKAGE BODY ORDER_PROCESS_PACKAGE AS

/*
SAVE_EXCEPTION is a private procedure used internally for the other procedures/functions to access it. 
Thus it need not be declared in the package specification.
We will get a compile error if the private procedure is referenced in the package body 
before it is declared or defined. To avoid that, declare the private procedure at the beginning of the package body 
(using the same kind of declaration you would use in the package head); then it can be defined anywhere in the package body. 
*/

 PROCEDURE SAVE_EXCEPTION (message varchar2, fhandle in out utl_file.file_type );

/*
 version: 2.1 ( Standard version)
 saves the order data into 3 tables and the Order Id is populated with the data.
*/
PROCEDURE SAVE_ORDER_SP (
    l_from_address IN  address_obj,
    l_to_address IN  address_obj,
    l_line_items IN  Lines_table,
    instructions IN  ORDERS.instructions%type,
    order_id OUT ORDERS.id%type,
    message OUT varchar2)
IS
    l_order_id number;
    l_from_address_id varchar2(50);
    l_to_address_id varchar2(50);
    l_row_count number;
    fhandle utl_file.file_type;
   
    address_insert_error EXCEPTION;
    order_insert_error EXCEPTION;
    LineItems_insert_error EXCEPTION;

    --utl_file related. TODO
    v_file_exist boolean;
    file_length number;
    block_size number;

BEGIN
    -- synta
    --  UTL_FILE.FGETATTR( location IN VARCHAR2, filename IN VARCHAR2, exists OUT BOOLEAN, file_length OUT NUMBER, blocksize OUT NUMBER);
    --  sys.utl_file.fgetattr('LOG_DIR',             'save_order_sp.log',     v_file_exist,       file_length,            block_size);
    fhandle := utl_file.fopen('LOG_DIR', 'save_order_sp.log', 'a');
    UTL_FILE.PUT_LINE(fhandle, '__________________________________________________________________________________');
    UTL_FILE.PUT_LINE(fhandle, 'Date= '|| to_char(sysdate,'yyyy-dd-mm hh:mi:ss'));

    dbms_output.put_line('inserting into the new values into address..');

    -- address_id is a combination of state and zip.
    l_from_address_id := UPPER(l_from_address.state) || UPPER(l_from_address.zip);

    select count(*) into l_row_count
      from ADDRESS
     where address_id=l_from_address_id;


     SAVEPOINT from_address_savepoint;
     
     if l_row_count=0
     then
        UTL_FILE.PUT_LINE(fhandle, '      The ''From'' address details not exists in ''ADDRESS'' so, inserting. l_from_address_id= '|| l_from_address_id);
        dbms_output.put_line('inserting into the new values into address.. l_from_address_id='|| l_from_address_id);

        insert into ADDRESS (address_id, city, state ,zip)
        values (l_from_address_id, l_from_address.city, l_from_address.state, l_from_address.zip);

     else
       dbms_output.put_line('from address already exists');
	 UTL_FILE.PUT_LINE(fhandle, '      ''From'' address already exists. l_from_address_id= '|| l_from_address_id);
     end if;

    -- insert into the Address table for 'TO' fields  
    l_to_address_id := UPPER(l_to_address.state) || UPPER(l_to_address.zip);
    select count(*) into l_row_count
      from ADDRESS
     where address_id=l_to_address_id;

    if l_row_count=0
    then
       dbms_output.put_line('inserting into the new TO address..');
       UTL_FILE.PUT_LINE(fhandle, '      The ''To'' address details not exist in ''ADDRESS'' so, inserting. l_to_address_id= '|| l_to_address_id);
       UTL_FILE.FFLUSH(fhandle);

       insert into ADDRESS (address_id, city, state ,zip)
       values (l_to_address_id, l_to_address.city, l_to_address.state, l_to_address.zip);

       if (SQL%ROWCOUNT =0)
       then
          UTL_FILE.PUT_LINE(fhandle, '      INSERT action failed for ''To'' address. l_to_address_id= '|| l_to_address_id);
          UTL_FILE.FFLUSH(fhandle);
          RAISE address_insert_error;
       end if;
    else
	 dbms_output.put_line('TO address already exists');
	 UTL_FILE.PUT_LINE(fhandle, '      ''To'' address already exists. l_to_address_id= '|| l_to_address_id);
    end if;

    UTL_FILE.PUT_LINE(fhandle, '      fetching from seq. new Order id');

    select order_id_seq.nextval into l_order_id from dual;

    order_id  :=  l_order_id; -- populate in the OUT param
    
    dbms_output.put_line('l_order_id=' || l_order_id);
    UTL_FILE.PUT_LINE(fhandle, '      fetched from seq. l_order_id= '|| l_order_id);
    UTL_FILE.FFLUSH(fhandle);
    
    -- inserting into table, ORDERS.
    UTL_FILE.PUT_LINE(fhandle, '      inserting data into table, ORDERS.');
    UTL_FILE.FFLUSH(fhandle);
   
    insert into ORDERS (id, from_address_id, to_address_id, instructions)
    values (l_order_id, l_from_address_id, l_to_address_id, instructions);

    l_row_count := SQL%ROWCOUNT;
    if l_row_count = 1
    then
           dbms_output.put_line('data inserted into ORDER');
           UTL_FILE.PUT_LINE(fhandle, '      data inserted into table, ORDERS.');
           UTL_FILE.FFLUSH(fhandle);
    else
           message := 'data Could NOT be inserted into ORDER';
           dbms_output.put_line('data NOT inserted into ORDER');
           UTL_FILE.PUT_LINE(fhandle, '      data NOT inserted into table, ORDERS.');
           UTL_FILE.FFLUSH(fhandle);
           raise order_insert_error;
    end if;

    --inserting data into LINE_ITEMS
    UTL_FILE.PUT_LINE(fhandle, '      inserting data into LINE_ITEMS table.');
    forall i in 1..l_line_items.count
        insert into LINE_ITEMS(order_id , weight,volume, hazard, product)
        values(l_order_id, l_line_items(i).weight,l_line_items(i).volume, l_line_items(i).hazard, l_line_items(i).product);                                

    l_row_count := SQL%ROWCOUNT;
    if l_row_count > 0
            then
            dbms_output.put_line('data inserted into line_items. row count' || l_row_count);
            UTL_FILE.PUT_LINE(fhandle, '      data inserted into line_items. row count=' || l_row_count);
            UTL_FILE.FFLUSH(fhandle);
    else
            message := 'data NOT inserted into line_items. transaction rolled back!';
            dbms_output.put_line('data NOT inserted into line_items');
            UTL_FILE.PUT_LINE(fhandle, '      data NOT inserted into line_items. row count=' || l_row_count);
            UTL_FILE.FFLUSH(fhandle);
            raise LineItems_insert_error;
    end if;                                       

     -- l_row_count := 100.00 /0; --test purpose

    commit; --this commits entire database activity.
    UTL_FILE.PUT_LINE(fhandle, '      All insert operations are completed. order_id= '|| l_order_id);

    UTL_FILE.fflush(fhandle);
    UTL_FILE.fclose(fhandle);                                                                             

    EXCEPTION
    WHEN address_insert_error THEN
      ROLLBACK TO from_address_savepoint;
      message := 'data could NOT be inserted into ADDRESS. transaction rolled back!';
      SAVE_EXCEPTION(message,fhandle);
    WHEN order_insert_error THEN
         ROLLBACK TO from_address_savepoint;
         message := 'data could NOT be inserted into Order. transaction rolled back!';
         SAVE_EXCEPTION(message,fhandle);
    WHEN LineItems_insert_error THEN
         ROLLBACK TO from_address_savepoint;
         message := 'data could NOT be inserted into LineItems. transaction rolled back!';
         SAVE_EXCEPTION(message,fhandle);
    WHEN ZERO_DIVIDE THEN
        message := SQLCODE || ': '|| SQLERRM || '. ZERO_DIVIDE error.! rollback the entire transaction.. done';
        ROLLBACK TO from_address_savepoint;
        SAVE_EXCEPTION(message,fhandle);
    WHEN NO_DATA_FOUND THEN
         ROLLBACK TO from_address_savepoint;
         message := 'A SELECT...INTO did not return any row.!';
         SAVE_EXCEPTION(message,fhandle);
    WHEN OTHERS THEN
         ROLLBACK TO from_address_savepoint;
         message := SQLCODE || ': '|| SQLERRM || '. Data Base Error Occured for the Order Id : ' || l_order_id;
         SAVE_EXCEPTION(message,fhandle);
 END SAVE_ORDER_SP;
 
 /*
  SP Version: Version 1(dynamic sql execution)
  Saves the order data into 3 tables and the Order Id is populated with the data.
  This version employs the dynamic sql execution for inserting data
 */
 
 PROCEDURE SAVE_ORDER_VER_1_SP
 (from_city IN ADDRESS.city%type,                                                
  from_state IN ADDRESS.state%type,                                                 
  from_zip IN ADDRESS.zip%type,                                               
  to_city IN ADDRESS.city%type,                                                     
  to_state IN ADDRESS.state%type,                                                
  to_zip IN ADDRESS.zip%type,                                                  
  dynamic_line_item_sql in OUT varchar2,                                         
  instructions IN  ORDERS.instructions%type,                                     
  order_id OUT ORDERS.id%type,                                                   
  message OUT varchar2)                                                          
 AS                                                                              
    l_order_id number;
    l_from_address_id varchar2(50);
    l_to_address_id varchar2(50);
    l_temp_str varchar2(5000);
    l_row_count number;
    fhandle utl_file.file_type;
    
    address_insert_error EXCEPTION; 
    order_insert_error EXCEPTION; 
    LineItems_insert_error EXCEPTION; 
                                                                                 
 BEGIN                                                                           
 
 
     fhandle := utl_file.fopen('LOG_DIR', 'save_order_ver_1_sp.log', 'a');
     UTL_FILE.PUT_LINE(fhandle, '__________________________________________________________________________________');
     UTL_FILE.PUT_LINE(fhandle, 'Date= '|| to_char(sysdate,'yyyy-dd-mm hh:mi:ss'));

                                      
         dbms_output.put_line('inserting into the new values into address..');    
                                                                                 
  -- address_id is a combination of state and zip.                                                                               
 select count(*) into l_row_count                                                
 from ADDRESS                                                                    
 where  -- UPPER(city)= UPPER(from_city) and                                             
   UPPER(state)=UPPER(from_state)  and                                          
    UPPER(zip)=UPPER(from_zip);                                               
                                                                                 
 if l_row_count=0                                                                
     then                                                                        
        l_from_address_id := from_state || from_zip;
        
     UTL_FILE.PUT_LINE(fhandle, '      The ''From'' address details not exists in ''ADDRESS'' so, inserting. l_from_address_id= '|| l_from_address_id);
        dbms_output.put_line('inserting into the new values into address..');    
 
     SAVEPOINT from_address_savepoint;
          
        insert into ADDRESS (address_id, city, state ,zip)                       
        values (l_from_address_id, from_city, from_state, from_zip);             
        commit;                                                                  
     else                                                                        
     select  address_id into l_from_address_id                                   
     from ADDRESS                                                                
     where -- UPPER(city)= UPPER(from_city)   and                                      
        UPPER(state)=UPPER(from_state)    and                                    
        UPPER(zip)=UPPER(from_zip);                                               
        dbms_output.put_line('from address already exists');                     
     end if;                                                                     
                                                                                 
     -- insert into the Address table for 'TO' fields                                                                                 
     select count(*) into l_row_count                                            
     from ADDRESS                                                                
     where UPPER(city)= UPPER(to_city)                                           
       and UPPER(state)=UPPER(to_state)                                          
       and UPPER(zip)=UPPER(to_zip);                                             
                                                                                 
     if l_row_count=0                                                            
         then                                                                    
            l_to_address_id := to_state || to_zip;                               
            dbms_output.put_line('inserting into the new TO address..');
            UTL_FILE.PUT_LINE(fhandle, '      The ''To'' address details not exists in ''ADDRESS'' so, inserting. l_to_address_id= '|| l_to_address_id);
 
            insert into ADDRESS (address_id, city, state ,zip)                   
            values (l_to_address_id, to_city, to_state, to_zip);
            if (SQL%ROWCOUNT =0)
            then
                RAISE address_insert_error; 
            end if;                                                             
         else                                                                    
         select address_id into l_to_address_id                                  
 		from ADDRESS                                                                  
 		where UPPER(city)= UPPER(to_city)                                             
 		  and UPPER(state)=UPPER(to_state)                                            
 		  and UPPER(zip)=UPPER(to_zip);                                               
 	 dbms_output.put_line('TO address already exists');                            
     end if;                                                                     

     UTL_FILE.PUT_LINE(fhandle, '      fetching from seq, order_id_seq.');
 
 select order_id_seq.nextval into l_order_id from dual;                          
 order_id  :=  l_order_id;                                                       
        dbms_output.put_line('l_order_id=' || l_order_id);
     UTL_FILE.PUT_LINE(fhandle, '      fetched from seq. l_order_id= '|| l_order_id);
     
     -- inserting into table, ORDERS.
     UTL_FILE.PUT_LINE(fhandle, '      inserting into table, ORDERS.');
 
     insert into ORDERS (id, from_address_id, to_address_id, instructions)       
     values (l_order_id, l_from_address_id, l_to_address_id, instructions);      
                                                                                 
     l_row_count := SQL%ROWCOUNT;                                                
     if l_row_count = 1                                                          
         then                                                                    
            dbms_output.put_line('data inserted into ORDER');
            UTL_FILE.PUT_LINE(fhandle, '      data inserted into table, ORDERS.');
         else                                                                    
            message := 'data NOT inserted into ORDER';                              
            dbms_output.put_line('data NOT inserted into ORDER');
            UTL_FILE.PUT_LINE(fhandle, '      data NOT inserted into table, ORDERS.');
                            
            raise order_insert_error;                                              
     end if;                                                                     
                                                                                 
     --inserting data into LINE_ITEMS
     UTL_FILE.PUT_LINE(fhandle, '      inserting data into LINE_ITEMS table.');
     if l_row_count = 1                                                          
        then                                                                                
           dbms_output.put_line(replace(dynamic_line_item_sql,':order_id_val',l_order_id));                                                                         
           dynamic_line_item_sql :=  replace(dynamic_line_item_sql,':order_id_val',l_order_id);
           l_temp_str := replace (dynamic_line_item_sql, '''', '''''');
           --UTL_FILE.PUT_LINE(fhandle, '      dynamic SQl for LINE_ITEMS table=' || '''');
           --UTL_FILE.PUT_LINE(fhandle, '      dynamic SQl for LINE_ITEMS table=' || replace(dynamic_line_item_sql,'''',''''''));
           -- UTL_FILE.PUT_LINE(fhandle, '      dynamic SQl for LINE_ITEMS table=' || dbms_assert.enquote_literal( l_temp_str));
           --UTL_FILE.PUT_LINE(fhandle, '      dynamic SQl for LINE_ITEMS table=' || l_temp_str);
           EXECUTE IMMEDIATE dynamic_line_item_sql; 
     end if;                                                                     
                                                                                 
     l_row_count := SQL%ROWCOUNT;                                                
     if l_row_count > 0                                                          
             then                                                                
             dbms_output.put_line('data inserted into line_items. row count' || l_row_count);   
             UTL_FILE.PUT_LINE(fhandle, '      data inserted into line_items. row count' || l_row_count);                                                                 
     else                                                                        
             message := 'data NOT inserted into line_items. transaction rolled back!';                                                                               
             dbms_output.put_line('data NOT inserted into line_items');  
             UTL_FILE.PUT_LINE(fhandle, '      data NOT inserted into line_items. row count' || l_row_count);             
             raise LineItems_insert_error;
     end if;                                                                     
   
     commit;
     UTL_FILE.PUT_LINE(fhandle, '      The procedure is executed successfully for the order id =' ||l_order_id);
     UTL_FILE.fclose(fhandle);                                                                              
     
     EXCEPTION 
     WHEN address_insert_error THEN
          ROLLBACK TO from_address_savepoint;
          message := 'data NOT inserted into ADDRESS. transaction rolled back!';
         SAVE_EXCEPTION(message,fhandle);
     WHEN order_insert_error THEN
          ROLLBACK TO from_address_savepoint;
          message := 'data NOT inserted into Order. transaction rolled back!';
         SAVE_EXCEPTION(message,fhandle);
     WHEN LineItems_insert_error THEN
          ROLLBACK TO from_address_savepoint;
          message := 'data NOT inserted into LineItems. transaction rolled back!';
         SAVE_EXCEPTION(message,fhandle);         
 
     WHEN NO_DATA_FOUND THEN 
          ROLLBACK TO from_address_savepoint;
          message := 'A SELECT...INTO did not return any row.!';
          SAVE_EXCEPTION(message,fhandle);
         
     WHEN OTHERS THEN 
	  message := SQLCODE || ': '|| SQLERRM || '. Data Base Error Occured for the Order Id : ' || l_order_id;
          SAVE_EXCEPTION(message,fhandle);
          ROLLBACK TO from_address_savepoint;
  END SAVE_ORDER_VER_1_SP; 
 
 
 /*
  * this is frequently used call.
 */
 PROCEDURE SAVE_EXCEPTION (message varchar2, fhandle in out utl_file.file_type )
 IS
 BEGIN
        dbms_output.put_line (message);
       if (UTL_FILE.is_open(fhandle))
       then
               UTL_FILE.PUT_LINE(fhandle, '     '||message);
               UTL_FILE.FFLUSH(fhandle);

               UTL_FILE.fclose(fhandle);
      end if;
 END SAVE_EXCEPTION;
 

/*
 Retrieve the data from multiple tables for the given order_id
 PLSQL types can't be accessed directly from java. we will have to use a true SQL type (CREATE type...) 
 and either a wrapper procedure with this SQL type or modify  procedure to accept the new type
*/
PROCEDURE LOOKUP_ORDER_SP (
    l_instructions out  ORDERS.instructions%type,
    l_line_items out  Lines_table,
    l_from_address out  address_obj,
    l_to_address out  address_obj,
    l_order_id IN  ORDERS.id%type,
    message IN out varchar2)
IS
   l_from_address_id varchar2(50);
   l_to_address_id varchar2(50);
   l_row_count number;
   
   fhandle utl_file.file_type;

BEGIN
    fhandle := utl_file.fopen('LOG_DIR', 'lookup_order_sp.log', 'a');
    UTL_FILE.PUT_LINE(fhandle, '__________________________________________________________________________________');
    UTL_FILE.PUT_LINE(fhandle, 'Date= '|| to_char(sysdate,'yyyy-dd-mm hh:mi:ss'));
    UTL_FILE.PUT_LINE(fhandle, '      l_order_id= '|| l_order_id);
    
    UTL_FILE.PUT_LINE(fhandle, '    Fetching data from ORDER..');
    Select instructions,  from_address_id, to_address_id into l_instructions, l_from_address_id, l_to_address_id
	from orders where id= l_order_id;

    UTL_FILE.PUT_LINE(fhandle, '    Done. fetching ORDER data. count=' || SQL%ROWCOUNT);
    
    UTL_FILE.PUT_LINE(fhandle, '    Fetching LINE_ITEMS..');
    UTL_FILE.FFLUSH(fhandle);
    
    --fetch Line items
    select LINEITEM_OBJECT( weight, volume, hazard, product)  bulk collect  into l_line_items from line_items where order_id=l_order_id;
    
    UTL_FILE.PUT_LINE(fhandle, '    Done. fetching LINE_ITEMS. count=' || l_line_items.count);
    
    dbms_output.put_line('data retrieved from LINE_ITEMS. ORDER_ID=' || l_order_id || '; row count=' || SQL%ROWCOUNT); 

    
    UTL_FILE.PUT_LINE(fhandle, '    Fetching data of "From" Address..');
    --fetch 'From' Address
    select ADDRESS_OBJ(city, state, zip) into l_from_address from ADDRESS where address_id=l_from_address_id;

    UTL_FILE.PUT_LINE(fhandle, '    Done. fetching "From" Address... count=' || SQL%ROWCOUNT);

    UTL_FILE.PUT_LINE(fhandle, '    Fetching data of "To" Address..');
    
    --fetch 'TO' Address
    select ADDRESS_OBJ(city, state, zip) into l_to_address from ADDRESS where address_id=l_to_address_id;

    UTL_FILE.PUT_LINE(fhandle, '    Done. fetching "To" Address... count=' || SQL%ROWCOUNT);
    UTL_FILE.fclose(fhandle);


EXCEPTION
   WHEN no_data_found THEN
      message := SQLCODE || ': '|| SQLERRM || '. The Order Id : ' || l_order_id || ' does not Exists in the System';
      SAVE_EXCEPTION(message,fhandle);
   WHEN others THEN
      message := SQLCODE || ': '|| SQLERRM || '. Data Base Error Occured while searching the Order Id : ' || l_order_id;
      SAVE_EXCEPTION(message,fhandle);      
END LOOKUP_ORDER_SP;

END ORDER_PROCESS_PACKAGE;
 /
 show errors;
/