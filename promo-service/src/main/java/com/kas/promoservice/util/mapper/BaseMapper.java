package com.kas.promoservice.util.mapper;

public interface BaseMapper <E, D> {
    D toDto(E entity);
    E toEntity(D dto);
}
