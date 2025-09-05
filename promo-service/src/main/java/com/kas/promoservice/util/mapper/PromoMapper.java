package com.kas.promoservice.util.mapper;

import com.kas.promoservice.dto.PromoDto;
import com.kas.promoservice.model.Promo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PromoMapper extends BaseMapper<Promo, PromoDto> {

}
