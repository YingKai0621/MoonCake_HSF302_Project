package com.se201411.project.mooncake.inventory.service;
import com.se201411.project.mooncake.inventory.repository.BatchRepository;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import com.se201411.project.mooncake.enums.StockMovementType;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class BatchStockServiceTest {
 final BatchRepository repo=mock(BatchRepository.class);
 final BatchStockService service=new BatchStockService(repo,mock(AdminAccessService.class));
 @Test void saleConsumesEligibleBatchesInExpiryOrder(){
  when(repo.physical(1)).thenReturn(7);when(repo.tracked(1)).thenReturn(12);
  when(repo.available(1,false)).thenReturn(List.of(new Object[]{11L,3},new Object[]{12L,9}));
  service.apply(1,-5,"SALE",9L,StockMovementType.SALE);
  var order=inOrder(repo);order.verify(repo).change(11,-3);order.verify(repo).record(11,9L,"SALE",-3);order.verify(repo).change(12,-2);
 }
 @Test void insufficientEligibleStockDoesNotConsumeAnyBatch(){
  when(repo.physical(1)).thenReturn(7);when(repo.tracked(1)).thenReturn(12);
  when(repo.available(1,false)).thenReturn(java.util.Collections.singletonList(new Object[]{11L,3}));
  assertThatThrownBy(()->service.apply(1,-5,"SALE",9L,StockMovementType.SALE)).isInstanceOf(BusinessRuleViolationException.class).hasMessageContaining("3");
  verify(repo,never()).change(anyLong(),anyInt());
 }
 @Test void cancellationRestoresOriginalLotsEvenWhenExpired(){
  when(repo.physical(1)).thenReturn(12);when(repo.tracked(1)).thenReturn(7);
  when(repo.sold(1,9)).thenReturn(List.of(new Object[]{11L,3},new Object[]{12L,2}));
  service.apply(1,5,"RETURN",9L,StockMovementType.CANCEL_RETURN);
  verify(repo).change(11,3);verify(repo).change(12,2);verify(repo,never()).create(anyLong(),anyString(),anyInt());
 }
 @Test void balanceMismatchFailsInsteadOfFabricatingLots(){
  when(repo.physical(1)).thenReturn(2);when(repo.tracked(1)).thenReturn(9);
  assertThatThrownBy(()->service.apply(1,-1,"SALE",9L,StockMovementType.SALE)).isInstanceOf(BusinessRuleViolationException.class);
 }
}
