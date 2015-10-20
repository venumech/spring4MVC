-- @C:\common\order_DDL.sql
-- Tables for Order Process system

drop TABLE VENU.LINE_ITEMS;
drop TABLE VENU.ORDERS;
drop TABLE VENU.ADDRESS;

  CREATE TABLE VENU.ADDRESS
   (    address_id varchar2(50) not null enable,
        city varchar2(100),
        state varchar2(20),
        zip varchar2(20) not null enable,
         CONSTRAINT ADDRESS_ID_PK PRIMARY KEY (address_id),
         CONSTRAINT ADDRESS_UK UNIQUE (city, state, zip)
   );

  CREATE TABLE VENU.ORDERS
   (    id number not null enable,
        from_address_id varchar2(50) not null enable,
        to_address_id varchar2(50) not null enable,
        instructions varchar2(1000),
        order_date date default sysdate,
         constraint pk_column primary key (id),
         CONSTRAINT FK1_COLUMN FOREIGN KEY (from_address_id)  REFERENCES VENU.ADDRESS (address_id) ENABLE,
         CONSTRAINT FK2_COLUMN FOREIGN KEY (to_address_id)    REFERENCES VENU.ADDRESS (address_id) ENABLE
   );


  CREATE TABLE VENU.LINE_ITEMS
   (    order_id number not null enable,
        weight number not null enable,
        volume number not null enable,
        hazard char(1),
        product varchar2(100) not null enable,
        CONSTRAINT FK_ORDER_ID FOREIGN KEY (order_id) REFERENCES VENU.ORDERS (id) ENABLE
   );

  --Sequence for retreaving orderid.
   CREATE SEQUENCE  "VENU"."ORDER_ID_SEQ"  MINVALUE 1 MAXVALUE 9999999999999999999999999999 
   INCREMENT BY 1 START WITH 1000 NOCACHE  NOORDER  NOCYCLE  NOPARTITION;