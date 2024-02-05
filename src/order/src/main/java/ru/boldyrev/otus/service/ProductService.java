package ru.boldyrev.otus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.boldyrev.otus.exception.ConflictException;
import ru.boldyrev.otus.model.entity.Product;
import ru.boldyrev.otus.model.transfer.TransportableProduct;
import ru.boldyrev.otus.repo.ProductRepo;

import java.util.Optional;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ProductService {
    private final ProductRepo productRepo;

    public TransportableProduct get(Long productId) throws ConflictException {

        Product product = productRepo.findById(productId).orElseThrow(()-> new ConflictException("Product not found"));
        return new TransportableProduct(product);
    }

    public TransportableProduct saveProduct(TransportableProduct tProduct) {
        Product product;
        if (tProduct.getId() == null) product = new Product(tProduct);
        else {
            product = productRepo.findById(tProduct.getId()).orElse(new Product(tProduct));
            product.setName(tProduct.getName()).setPrice(tProduct.getPrice()).setId(tProduct.getId());
        }

        product = productRepo.save(product);
        return new TransportableProduct(product);
    }

    public boolean delete(Long productId) {
        if (productRepo.findById(productId).isPresent()){
            productRepo.deleteById(productId);
            return true;
        } else {
            return false;
        }
    }
}
