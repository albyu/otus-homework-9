package ru.boldyrev.otus.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.boldyrev.otus.model.entity.Account;

import java.util.Optional;

public interface AccountRepo extends JpaRepository<Account, Long> {

    //    @Query("SELECT o FROM OrderConfirmRequest o WHERE o.orderCaseId = :orderCaseId and o.orderRequestType = :orderRequestType " +
//            "and o.systemName = :systemName")
//    List<OrderConfirmRequest> findByComposedKey(@Param("orderCaseId")Long orderCaseId,
//                                                @Param("orderRequestType")OrderRequestType orderRequestType,
//                                                @Param("systemName")OtusSystem systemName);
//
//    List<OrderConfirmRequest> findByOrderId (String orderId);
    Optional<Account> findByUserName(String userName);

}
