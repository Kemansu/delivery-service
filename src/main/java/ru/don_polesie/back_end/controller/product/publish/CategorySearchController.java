package ru.don_polesie.back_end.controller.product.publish;


import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.security.RolesAllowed;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.don_polesie.back_end.model.product.Brand;
import ru.don_polesie.back_end.model.product.Category;
import ru.don_polesie.back_end.service.product.BrandService;
import ru.don_polesie.back_end.service.product.CategoryService;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/find/category")
public class CategorySearchController {
    private CategoryService categoryService;

    @Operation(
            summary = "Получить список категорий продуктов"
    )
    @GetMapping
    public ResponseEntity<List<Category>> getBrands(){
        return ResponseEntity.ok(categoryService.findAll());
    }

    @Operation(
            summary = "Получить список неактивных категорий продуктов"
    )
    @RolesAllowed({"ADMIN", "WORKER"})
    @GetMapping("/deactivated")
    public ResponseEntity<List<Category>> getDeactivatedBrands(){
        return ResponseEntity.ok(categoryService.findAllDeactivated());
    }
}
