package kr.hhplus.be.server.application.product.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.domain.product.entity.Stock;
import kr.hhplus.be.server.domain.product.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class StockService {

    private final StockRepository stockRepository;
    /**
     * 출고 이력 기록
     */
    public Stock recordOut(Long productId, Integer quantity, String reason){
        Stock stock = Stock.createOut(productId, quantity, reason);
        return stockRepository.save(stock);
    }
    /**
     * 입고 이력 기록 (재고 복구 시)
     */
    public Stock recordIn(Long productId, Integer quantity, String reason){
        Stock stock = Stock.createIn(productId, quantity, reason);
        return stockRepository.save(stock);
    }
    
}
