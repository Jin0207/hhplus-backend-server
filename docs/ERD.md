Table users {
  id integer [primary key, note: '식별자']
  user_id varchar [unique, not null, note: '유저 ID']
  password varchar [not null, note: '유저 PWD']
  point  integer [default: 0, note: '보유 포인트']
  crt_dttm datetime [note: '생성일']
  upd_dttm datetime [note: '변경일']
}

Table point_Histories {
  id integer [primary key, note: '식별자']
  user_id varchar [not null, ref: > users.id, note: '유저ID FK']
  point integer [not null, note: '포인트']
  type varchar [not null, note: 'USE:사용, CHARGE:충전']
  comment varchar [note: '비고']
  crt_dttm datetime [note: '생성일']
  upd_dttm datetime [note: '변경일']
}

Table coupons{
  id integer [primary key, note: '식별자']
  name varchar [not null, note: '쿠폰명']
  type varchar [not null, note: '할인타입(AMOUNT:금액, PERCENT:%)']
  discount_value integer [not null, note: '할인 금액 또는 퍼센트']
  min_order_price integer [not null, note: '최소 주문 금액']
  valid_from datetime [note: '유효시작일']
  valid_to datetime [note: '유효종료일']
  quantity integer [note: '총 발행 수량']
  available_quantity integer [note: '남은 수량']
  status varchar [note: '쿠폰 상태(ACTIVE:활성화, UNACTIVE:비활성화)']
  crt_dttm datetime [note: '생성일']
  upd_dttm datetime [note: '변경일']
}

Table user_coupons{
  user_id integer [not null, ref: > users.id]
  coupon_id integer [not null, ref: > coupons.id] 
  is_used tinyint(1) [default: 0, note: '사용여부']
  used_dttm datetime [note: '사용일시']
  crt_dttm datetime [note: '생성일']
  upd_dttm datetime [note: '변경일']
  indexes {
    (user_id, coupon_id) [pk]
  }
}

Table products {
  id integer [primary key, note: '식별자']
  product_name varchar [not null, note: '상품']
  price integer [not null, note: '가격']
  stock integer [not null, note: '재고수량']
  category varchar [note: '카테고리']
  status varchar [note: '판매 상태(ON_SALE:판매중, SOLD_OUT:품절, INACTIVE:판매종료)']
  sales_quantity integer [default: 0, note: '누적판매량']
  crt_dttm datetime [note: '생성일']
  upd_dttm datetime [note: '변경일']
}

Table stocks {
  id integer
  product_id integer [not null, ref: > products.id, note: '상품id(FK']
  quantity integer [not null, note: '입출고 수량 (+입고, -출고)']
  crt_dttm datetime [note: '생성일']
  upd_dttm datetime [note: '변경일']
}

Table orders{
  id integer [primary key]
  user_id integer [not null, ref: > users.id, note: '복합 FK (user_id)' ]
  coupon_id integr [null,  ref: > user_coupons.coupon_id, note: '복합 FK (coupon_id)']
  total_price integer [not null, note: '총 주문 금액']
  discount_price integer [default: 0, note: '할인 금액']
  final_price integer [not null, note: '결제 금액 (할인 후)']
  order_status varchar [note: '주문 상태(PENDING:진행중(대기), COMPLETED:완료, CANCELED:취소)']
  crt_dttm datetime [note: '생성일']
  upd_dttm datetime [note: '변경일']
}

Table order_detail{
  id integer [primary key, note:'식별자']
  order_id integer [not null, ref: > orders.id, note: '주문번호']
  product_id integr [not null, ref: > products.id, note: '상품번호']
  quantity integer [not null, note:'수량']
  subtotal integer [not null, note:'소계']
  crt_dttm datetime [note: '생성일']
  upd_dttm datetime [note: '변경일']
}

Table payments {
  id integer [primary key, note: '식별자']
  order_id integer [not null, ref: > orders.id, note: '주문ID FK']
  user_id integer [not null, ref: > users.id, note: '유저ID FK']
  price integer [not null, note: '결제 금액']
  status varchar [not null, note: '결제 상태(PENDING:진행중(대기), COMPLETED:완료, CANCELED:취소)']
  payment_type varchar [note: '결제 수단']
  payment_gateway varchar [note: '결제 게이트웨이']
  transaction_id varchar [unique, note: '거래 ID']
  fail_reson varchar [note: '실패 사유']
  request_dttm datetime [note: '요청일시']
  success_dttm datetime [note: '성공일시']
  external_sync tinyint(1)  [default: 0, note: '외부 동기화 여부']
  synced_dttm datetime [note: '동기화일시']
  crt_dttm datetime [note: '생성일']
  upd_dttm datetime [note: '변경일']
}