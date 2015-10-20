-- @C:\common\order_Collections.sql
-- collection objects used in the stored procedures
drop TYPE LINEITEM_OBJECT force;

drop TYPE LINES_TABLE force; //Collection of 1 or many LineItem_object



CREATE OR REPLACE TYPE LINEITEM_OBJECT as object(
  --order_id   number,
    weight   number,
    volume   number,
    hazard   char(1), --boolean not supported
    product  varchar2(100)
);
/

CREATE OR REPLACE TYPE LINES_TABLE is table of LINEITEM_OBJECT;
/

drop TYPE address_t force;

drop TYPE address_obj force;

CREATE OR REPLACE TYPE ADDRESS_OBJ as object(
    city  varchar2(100),
    state varchar2(20),
    zip varchar2(20)
    
);
/
show errors;
/

