-- @C:\common\test_proc2.sql

VARIABLE message varchar2;
VARIABLE order_id number;
VARIABLE to_zip varchar2;
VARIABLE from_zip varchar2;
VARIABLE to_state varchar2;
VARIABLE from_state varchar2;
VARIABLE to_city varchar2;
VARIABLE from_city varchar2;
VARIABLE instructions varchar2;
REFCURSOR l_col_data LINES_TABLE;

DECLARE
      from_city ADDRESS.city%type;
      from_zip ADDRESS.zip%type;
      from_state ADDRESS.state%type;
      to_city  ADDRESS.city%type;
      to_zip  ADDRESS.zip%type;
      to_state  ADDRESS.state%type;
       l_col_data my_table;
      instructions   ORDERS.instructions%type;
      order_id  ORDERS.id%type;
    message  varchar2(1000);
    sql_str varchar2(1000);
BEGIN
sql_str  :=  'INSERT ALL INTO LINE_ITEMS (order_id , weight, volume, hazard, product ) VALUES (:order_id_val, 1000.1, 1.0, ''Y'', ''petrol'') INTO LINE_ITEMS (order_id , weight, volume, hazard, product ) VALUES (:order_id_val, 2000.0, 2.0, ''N'', ''water'') SELECT 1 FROM dual';
order_id := 27360;
message  := 'x';
to_zip := '';

    select LINEITEM_OBJECT(27360, 10,30, 'N',  'FLOGAN') bulk collect into l_col_data  from dual connect by level <=4;

     select LINEITEM_OBJECT(order_id , weight, volume, hazard, product)  bulk collect  into l_col_data from line_items;

    DBMS_OUTPUT.PUT_LINE(l_col_data(1).order_id || ' ' || l_col_data(1).product); -- display details
   DBMS_OUTPUT.PUT_LINE(l_col_data.count);
      insert_mydata(l_col_data);

ORDER_PROCESS_LOOKUP (  :from_city, :from_zip, :from_state, :to_city, :to_zip, :to_state, :l_col_data, :instructions, 27360, :message);
  --  ORDER_PROCESS_PROC1    ( 'COLORADAO SPRINGS', '80817',   'CO', 'Honolulu', '96621', 'HI', l_col_data, 'instructions', :order_id, :message);
   -- VENU.ORDER_PROCESS_PROC('COLORADAO SPRINGS', '80817',   'CO', 'Honolulu', '96621', 'HI', sql_str, 'instructions', :order_id, :message);
   -- VENU.ORDER_PROCESS_PROC1('COLORADAO SPRINGS', '80817',   'CO', 'Honolulu', '96621', 'HI', sql_str, 'instructions', :order_id, :message);
 
END;
/
