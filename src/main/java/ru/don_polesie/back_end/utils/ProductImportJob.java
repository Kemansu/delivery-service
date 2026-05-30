package ru.don_polesie.back_end.utils;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import ru.don_polesie.back_end.model.product.Brand;
import ru.don_polesie.back_end.model.product.Category;
import ru.don_polesie.back_end.model.product.Product;
import ru.don_polesie.back_end.repository.BrandRepository;
import ru.don_polesie.back_end.repository.CategoryRepository;
import ru.don_polesie.back_end.repository.ProductRepository;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductImportJob {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    private static final String XML_PATH = "products.xml";

    @PostConstruct
    public void runOnStart() {
        importProductsFromXml(); // при старте
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void importProductsFromXml() {
        log.info("Начат импорт товаров из XML");

        try {
            File xmlFile = new File(XML_PATH);
            if (!xmlFile.exists()) {
                log.warn("Файл {} не найден", XML_PATH);
                return;
            }

            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList productNodes = doc.getElementsByTagName("product");

            for (int i = 0; i < productNodes.getLength(); i++) {
                Node node = productNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    processProduct(element);
                }
            }

            log.info("Импорт товаров из XML завершён");

        } catch (Exception e) {
            log.error("Ошибка при импорте товаров из XML", e);
        }
    }

    private void processProduct(Element element) {
        try {
            String uuidStr = getTagValue("uuid", element);
            if (uuidStr == null || uuidStr.isBlank()) {
                log.warn("Пропущен товар без UUID");
                return;
            }

            UUID uuid = UUID.fromString(uuidStr);
            Product existingProduct = productRepository.findAll().stream()
                    .filter(p -> uuid.equals(p.getUuid()))
                    .findFirst()
                    .orElse(null);

            if (existingProduct != null) {
                updateProduct(existingProduct, element);
                log.info("Обновлён товар с UUID: {}", uuid);
            } else {
                createProduct(element, uuid);
                log.info("Создан новый товар с UUID: {}", uuid);
            }

        } catch (Exception e) {
            log.error("Ошибка обработки товара", e);
        }
    }

    private void createProduct(Element element, UUID uuid) {
        Product product = new Product();
        product.setUuid(uuid);
        fillProductFields(product, element);
        productRepository.save(product);
    }

    private void updateProduct(Product product, Element element) {
        fillProductFields(product, element);
        productRepository.save(product);
    }

    private void fillProductFields(Product product, Element element) {
        product.setName(getTagValue("name", element));
        product.setPrice(new BigDecimal(Objects.requireNonNull(getTagValue("price", element))));
        product.setImageUrl(getTagValue("imageUrl", element));

        product.setFatGrams(new BigDecimal(Objects.requireNonNull(getTagValue("fatGrams", element))));
        product.setProteinGrams(new BigDecimal(Objects.requireNonNull(getTagValue("proteinGrams", element))));
        product.setCarbohydrateGrams(new BigDecimal(Objects.requireNonNull(getTagValue("carbohydrateGrams", element))));
        product.setEnergyKcalPer100g(new BigDecimal(Objects.requireNonNull(getTagValue("energyKcalPer100g", element))));

        // Обработка бренда
        String brandName = getTagValue("brand", element);
        Brand brand = brandRepository.findByName(brandName)
                .orElseGet(() -> brandRepository.save(new Brand(brandName)));
        product.setBrand(brand);

        // Обработка категории
        String categoryName = getTagValue("category", element);
        Category category = categoryRepository.findByName(categoryName)
                .orElseGet(() -> categoryRepository.save(new Category(categoryName)));
        product.setCategory(category);

        String minWeight = getTagValue("minWeight", element);
        if (minWeight != null && !minWeight.isBlank()) {
            product.setMinWeight(Integer.valueOf(minWeight));
        }

        product.setMaxWeight(Integer.valueOf(Objects.requireNonNull(getTagValue("maxWeight", element))));
        product.setAmount(Integer.valueOf(Objects.requireNonNull(getTagValue("amount", element))));
        product.setStorageTemperatureMin(Integer.valueOf(Objects.requireNonNull(getTagValue("storageTemperatureMin", element))));
        product.setStorageTemperatureMax(Integer.valueOf(Objects.requireNonNull(getTagValue("storageTemperatureMax", element))));
        product.setIsWeighted(Boolean.valueOf(getTagValue("isWeighted", element)));
        product.setCountryOfOrigin(getTagValue("countryOfOrigin", element));
        product.setComposition(getTagValue("composition", element));
        product.setDescription(getTagValue("description", element));
        product.setShelfLife(getTagValue("shelfLife", element));

        String sale = getTagValue("sale", element);
        if (sale != null && !sale.isBlank()) {
            product.setSale(Integer.valueOf(sale));
        }

        product.setActive(true);
    }

    private String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList.getLength() > 0 && nodeList.item(0).getFirstChild() != null) {
            return nodeList.item(0).getFirstChild().getNodeValue();
        }
        return null;
    }
}
