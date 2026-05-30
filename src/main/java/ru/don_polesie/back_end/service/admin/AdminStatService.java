package ru.don_polesie.back_end.service.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.don_polesie.back_end.dto.admin.RevenueDtoResponse;
import ru.don_polesie.back_end.dto.order.response.PopularProductDtoResponse;
import ru.don_polesie.back_end.repository.OrderProductRepository;
import ru.don_polesie.back_end.repository.OrderRepository;

import java.math.BigDecimal;
import java.time.*;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminStatService {

    private final OrderRepository orderRepository;
    private final OrderProductRepository orderProductRepository;

    private ZoneId zone = ZoneId.of("Europe/Moscow");


    public RevenueDtoResponse getDailyRevenue(int year, int month, int day) {
        LocalDate date = LocalDate.of(year, month, day);
        LocalDateTime startOfDay = date.atStartOfDay(zone).toLocalDateTime();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toLocalDateTime();


        String period = String.format("%d-%02d-%02d", year, month, day);
        BigDecimal revenue = orderRepository.calculateRevenueByPeriod(startOfDay, endOfDay);
        return new RevenueDtoResponse(revenue != null ? revenue.longValue() : 0, period);
    }


    public RevenueDtoResponse getMonthlyRevenue(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay(zone).toLocalDateTime();
        LocalDateTime endOfMonth = yearMonth.atEndOfMonth().plusDays(1).atStartOfDay(zone).toLocalDateTime();

        String period = String.format("%d-%02d", year, month);
        BigDecimal revenue = orderRepository.calculateRevenueByPeriod(startOfMonth, endOfMonth);
        return new RevenueDtoResponse(revenue != null ? revenue.longValue() : 0, period);
    }


    public Page<PopularProductDtoResponse> getMostPopularProducts(int year, int month, int pageNumber) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay(zone).toLocalDateTime();
        LocalDateTime endOfMonth = yearMonth.atEndOfMonth().plusDays(1).atStartOfDay(zone).toLocalDateTime();

        Pageable pageable = PageRequest.of(pageNumber, 10);

        return orderProductRepository.findPopularProductsByPeriod(startOfMonth, endOfMonth, pageable);
    }


    public Long getOrderCountPerDay(int year, int month, int day) {
        LocalDate date = LocalDate.of(year, month, day);
        LocalDateTime startOfDay = date.atStartOfDay(zone).toLocalDateTime();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay(zone).toLocalDateTime();

        return orderRepository.countByCreatedAtBetween(startOfDay, endOfDay);
    }


    public Long getOrderCountPerMonth(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay(zone).toLocalDateTime();
        LocalDateTime endOfMonth = yearMonth.atEndOfMonth().plusDays(1).atStartOfDay(zone).toLocalDateTime();

        return orderRepository.countByCreatedAtBetween(startOfMonth, endOfMonth);
    }
}
