package com.example.demo.repository;

import java.util.List;

public interface OrderESRepositoryCustom {
    List<String> findDistinctActiveProductsES();

    List<String> findDistinctActiveProductsESNative();
}