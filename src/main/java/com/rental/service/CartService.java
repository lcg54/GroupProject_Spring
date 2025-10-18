package com.rental.service;


import com.rental.entity.Cart;
import com.rental.entity.Member;
import com.rental.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;




}
