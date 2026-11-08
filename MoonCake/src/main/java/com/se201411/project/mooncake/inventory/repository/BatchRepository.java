package com.se201411.project.mooncake.inventory.repository;
import java.time.LocalDate;
import java.util.List;
public interface BatchRepository {
 int physical(long variant);
 int tracked(long variant);
 long create(long variant,String code,int quantity);
 List<Object[]> available(long variant,boolean expiredAllowed);
 List<Object[]> sold(long variant,long order);
 void change(long batch,int delta);
 void record(long batch,Long order,String key,int delta);
 List<Object[]> list();
 void finishPack(String group,long variant);
 void dates(long id,String code,LocalDate production,LocalDate expiry);
}
