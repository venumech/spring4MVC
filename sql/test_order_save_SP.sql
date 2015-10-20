-- @C:\common\order_save_testSP.sql

CREATE OR REPLACE PROCEDURE order_save_testSP
AS

      l_from_address   address_obj;
      l_to_address   address_obj;
       l_col_data LINES_TABLE;
      instructions   ORDERS.instructions%type;
      order_id  ORDERS.id%type;
    message  varchar2(1000);
    sql_str varchar2(1000);
BEGIN
sql_str  :=  'INSERT ALL INTO LINE_ITEMS (order_id , weight, volume, hazard, product ) VALUES (:order_id_val, 1000.1, 1.0, ''Y'', ''petrol'') INTO LINE_ITEMS (order_id , weight, volume, hazard, product ) VALUES (:order_id_val, 2000.0, 2.0, ''N'', ''water'') SELECT 1 FROM dual';


   select ADDRESS_OBJ('COLORADAO SPRINGS', '80817', 'CO') into l_from_address  from dual;
   select ADDRESS_OBJ('Honolulu', '96621', 'HI') into l_to_address  from dual;
    select LINEITEM_OBJECT( 10,30, 'N',  'FLOGAN') bulk collect into l_col_data  from dual connect by level <=4;
     
      dbms_output.put_line ('l_from_address,city '|| l_from_address.city);

--ORDER_PROCESS_LOOKUP (  :from_city, :from_zip, :from_state, :to_city, :to_zip, :to_state, :l_col_data, :instructions, 27360, :message);
  --ORDER_PROCESS_PROC    ( 'COLORADAO SPRINGS', '80817',   'CO', 'Honolulu', '96621', 'HI', sql_str, 'instructions', order_id, message);
  VENU.ORDER_PROCESS_PROC(l_from_address, l_to_address, l_col_data, 'instructions', order_id, message);
   -- VENU.ORDER_PROCESS_PROC1('COLORADAO SPRINGS', '80817',   'CO', 'Honolulu', '96621', 'HI', sql_str, 'instructions', :order_id, :message);
 
END;
/
show errors;
/