package kr.hhplus.be.server.domain.product.repository;

import java.util.List;

import kr.hhplus.be.server.domain.product.entity.Stock;

public interface StockRepository {
    Stock save(Stock stock);
}
