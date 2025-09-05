package com.kas.promoservice.controller;

import com.kas.promoservice.dto.PromoDto;
import com.kas.promoservice.service.PromoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@RestController
@RequestMapping("api/v1/promotions")
public class PromoController {

    private final PromoService promoService;

    @Operation(summary = "Получить список промо-акций",
            description = "Возвращает список промо-акций",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Все промо-акции",
                            content = @Content(schema = @Schema(implementation = PromoDto.class)))
            })
    @GetMapping
    public Flux<PromoDto> getPromos(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        return promoService.getPaginatedPromos(page, size);
    }

    @Operation(summary = "Получить промо-акцию по id",
            description = "Возвращает промо-акцию по id",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Детали промо-акции",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = PromoDto.class))),
                    @ApiResponse(responseCode = "404", description = "Промо-акция не найдена",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = PromoDto.class)))
            })
    @GetMapping("{id}")
    public Mono<PromoDto> getPromoById(@PathVariable String id) {
        return promoService.getPromoById(id);
    }

    @Operation(summary = "Создать промо-акцию",
            description = "Сохраняет промо-акцию в базе данных",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Детали промо-акции",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = PromoDto.class)))
            })
    @PostMapping
    public Mono<PromoDto> createPromo(@Valid @RequestBody PromoDto promoDto) {
        return promoService.savePromo(promoDto);
    }

    @Operation(summary = "Обновить промо-акцию",
            description = "Обновляет промо-акцию в базе данных",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Детали промо-акции",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = PromoDto.class))),
                    @ApiResponse(responseCode = "404", description = "Промо-акция не найдена",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = PromoDto.class)))
            })
    @PutMapping("{id}")
    public Mono<PromoDto> updatePromo(@Valid @RequestBody PromoDto promoDto, @PathVariable String id) {
        return promoService.updatePromo(promoDto, id);
    }

    @Operation(summary = "Удалить промо-акцию",
            description = "Удаляет промо-акцию",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Промо-акция удалена",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = PromoDto.class))),
                    @ApiResponse(responseCode = "404", description = "Промо-акция не найдена",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = PromoDto.class)))
            })
    @DeleteMapping("{id}")
    public Mono<Void> deletePromo(@PathVariable String id) {
        return promoService.deletePromo(id);
    }
}
