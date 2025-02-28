package com.example.myapp.controller;

import com.example.myapp.entity.Order;
import com.example.myapp.entity.OrderItem;
import com.example.myapp.entity.Product;
import com.example.myapp.repository.OrderRepository;
import com.example.myapp.repository.ProductRepository;
import com.example.myapp.repository.RefundRecordRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Controller
@RequestMapping("/orders")
public class OrderWebController {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderWebController(OrderRepository orderRepository, ProductRepository productRepository,
            RefundRecordRepository refundRecordRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @GetMapping
    public String listOrders(Model model) {
        List<Order> orders = orderRepository.findAll();
        model.addAttribute("orders", orders);
        return "orders";
    }
}
