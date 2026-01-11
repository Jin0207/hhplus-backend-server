package kr.hhplus.be.server.infrastructure.product.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import kr.hhplus.be.server.domain.order.enums.OrderStatus;
import kr.hhplus.be.server.domain.product.entity.ProductSearch;
import kr.hhplus.be.server.domain.product.enums.ProductCategory;
import kr.hhplus.be.server.domain.product.enums.ProductStatus;
import kr.hhplus.be.server.infrastructure.order.persistence.QOrderDetailEntity;
import kr.hhplus.be.server.infrastructure.order.persistence.QOrderEntity;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProductCustomRepositoryImpl implements ProductCustomRepository {

    private final JPAQueryFactory queryFactory;
    private final QProductEntity product = QProductEntity.productEntity;
    
    @Override
    public Page<ProductEntity> findBySearch(ProductSearch search, Pageable pageable) {
        // 1. 메인 쿼리 실행
        List<ProductEntity> content = queryFactory
                .selectFrom(product)
                .where(
                    productNameContains(search.productName()),
                    priceGoe(search.minPrice()),
                    priceLoe(search.maxPrice()),
                    categoryEq(search.category()),             
                    statusEq(search.status())                  
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(getOrderSpecifier(pageable.getSort()))
                .fetch();

        // 2. 카운트 쿼리
        JPAQuery<Long> countQuery = queryFactory
                .select(product.count())
                .from(product)
                .where(
                    productNameContains(search.productName()),
                    priceGoe(search.minPrice()),
                    priceLoe(search.maxPrice()),
                    categoryEq(search.category()),
                    statusEq(search.status())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }
    
    @Override
    public List<ProductEntity> findPopularProducts() {
        QOrderDetailEntity orderItem = QOrderDetailEntity.orderDetailEntity;
        QOrderEntity order = QOrderEntity.orderEntity;
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);

        return queryFactory
                .select(product)
                .from(product)
                .innerJoin(orderItem).on(orderItem.productId.eq(product.id))
                .innerJoin(order).on(order.id.eq(orderItem.orderId))
                .where(
                    order.crtDttm.goe(threeDaysAgo),            // 최근 3일 이내
                    order.orderStatus.ne(OrderStatus.CANCELED) // 취소된 주문 제외
                )
                .groupBy(product.id)
                .orderBy(orderItem.quantity.sum().desc()) // 누적 판매량 순
                .limit(5)
                .fetch();
    }
    // null 체크 및 빈 문자열 체크
    private BooleanExpression productNameContains(String productName) {
        return StringUtils.hasText(productName) ? product.productName.containsIgnoreCase(productName) : null;
    }

    private BooleanExpression priceGoe(Integer minPrice) {
        return minPrice != null ? product.price.goe(minPrice) : null;
    }

    private BooleanExpression priceLoe(Integer maxPrice) {
        return maxPrice != null ? product.price.loe(maxPrice) : null;
    }

    private BooleanExpression categoryEq(ProductCategory category) {
        return category != null ? product.category.eq(category) : null;
    }

    private BooleanExpression statusEq(ProductStatus status) {
        return status != null ? product.status.eq(status) : null;
    }
    
    private OrderSpecifier<?>[] getOrderSpecifier(Sort sort) {
        return sort.stream()
                .map(o -> {
                    Order direction = o.isAscending() ? Order.ASC : Order.DESC;
                    return switch (o.getProperty()) {
                        case "price" -> new OrderSpecifier<>(direction, product.price);
                        case "productName", "name" -> new OrderSpecifier<>(direction, product.productName);
                        case "crtDttm" -> new OrderSpecifier<>(direction, product.crtDttm);
                        default -> new OrderSpecifier<>(direction, product.id);
                    };
                })
                .toArray(OrderSpecifier[]::new);
    }
}
