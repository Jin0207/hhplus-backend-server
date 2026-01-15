package kr.hhplus.be.server.infrastructure.product.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;

import kr.hhplus.be.server.domain.order.enums.OrderStatus;
import kr.hhplus.be.server.domain.product.entity.PopularProduct;
import kr.hhplus.be.server.infrastructure.order.persistence.QOrderDetailEntity;
import kr.hhplus.be.server.infrastructure.order.persistence.QOrderEntity;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PopularProductCustomRepositoryImpl implements PopularProductCustomRepository {

    private final JPAQueryFactory queryFactory;
    private final QProductEntity product = QProductEntity.productEntity;
    private final QOrderDetailEntity orderDetail = QOrderDetailEntity.orderDetailEntity;
    private final QOrderEntity order = QOrderEntity.orderEntity;

    @Override
    public List<PopularProduct> aggregateTopSellingProducts(
        LocalDate startDate,
        LocalDate endDate,
        int limit,
        LocalDate baseDate
    ) {
        // 시작일 00:00:00 ~ 종료일 23:59:59
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<Tuple> results = queryFactory
            .select(
                product.id,
                product.productName,
                product.price,
                product.category,
                orderDetail.quantity.sum()
            )
            .from(product)
            .innerJoin(orderDetail).on(orderDetail.productId.eq(product.id))
            .innerJoin(order).on(order.id.eq(orderDetail.orderId))
            .where(
                order.crtDttm.goe(startDateTime),
                order.crtDttm.loe(endDateTime),
                order.orderStatus.ne(OrderStatus.CANCELED)
            )
            .groupBy(product.id, product.productName, product.price, product.category)
            .orderBy(orderDetail.quantity.sum().desc())
            .limit(limit)
            .fetch();

        List<PopularProduct> popularProducts = new ArrayList<>();
        int rank = 1;

        for (Tuple tuple : results) {
            PopularProduct popularProduct = PopularProduct.fromAggregation(
                rank++,
                tuple.get(product.id),
                tuple.get(product.productName),
                tuple.get(product.price),
                tuple.get(product.category),
                tuple.get(orderDetail.quantity.sum()),
                baseDate
            );
            popularProducts.add(popularProduct);
        }

        return popularProducts;
    }
}
