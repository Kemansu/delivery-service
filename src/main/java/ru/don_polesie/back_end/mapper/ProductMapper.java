package ru.don_polesie.back_end.mapper;

import org.mapstruct.*;
import ru.don_polesie.back_end.dto.product.ProductDtoFull;
import ru.don_polesie.back_end.dto.product.ProductDtoXML;
import ru.don_polesie.back_end.model.product.Brand;
import ru.don_polesie.back_end.model.product.Category;
import ru.don_polesie.back_end.model.product.Product;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    @Mapping(source = "brand.name", target = "brand")
    @Mapping(source = "brand.id", target = "brandId")
    @Mapping(source = "category.name", target = "category")
    @Mapping(source = "category.id", target = "categoryId")
    ProductDtoFull toProductDtoRR(Product product);

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "brand", target = "brand")
    @Mapping(source = "category", target = "category")
    @Mapping(source = "isWeighted", target = "isWeighted")
    Product productDtoXMLtoProduct(ProductDtoXML productDtoXML);

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "brand", target = "brand")
    @Mapping(source = "category", target = "category")
    Product productDtoRRtoProduct(ProductDtoFull productDtoFull);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(source = "brand", target = "brand")
    @Mapping(source = "category", target = "category")
    void updateFromDto(ProductDtoXML dto, @MappingTarget Product entity);

    // Кастомные методы маппинга для Brand
    default String mapBrandToString(Brand brand) {
        if (brand == null) {
            return null;
        }
        return brand.getName(); // или другое поле, которое представляет бренд как строку
    }

    default Brand mapStringToBrand(String brandName) {
        if (brandName == null || brandName.isEmpty()) {
            return null;
        }
        Brand brand = new Brand();
        brand.setName(brandName);
        return brand;
    }

    // Кастомные методы маппинга для Category
    default String mapCategoryToString(Category category) {
        if (category == null) {
            return null;
        }
        return category.getName(); // или другое поле
    }

    default Category mapStringToCategory(String categoryName) {
        if (categoryName == null || categoryName.isEmpty()) {
            return null;
        }
        Category category = new Category(categoryName);
        category.setName(categoryName);
        return category;
    }
}