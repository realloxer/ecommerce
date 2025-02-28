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
    private final RefundRecordRepository refundRecordRepository;

    public OrderWebController(OrderRepository orderRepository, ProductRepository productRepository,
            RefundRecordRepository refundRecordRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.refundRecordRepository = refundRecordRepository;
    }

    @GetMapping
    public String listOrders(Model model) {
        List<Order> orders = orderRepository.findAll();
        model.addAttribute("orders", orders);
        return "orders";
    }

    @GetMapping("/{id}")
    public String orderDetails(@PathVariable UUID id, Model model) {
        return orderRepository.findById(id)
                .map(order -> {
                    model.addAttribute("order", order);
                    model.addAttribute("refunds", refundRecordRepository.findByOrderId(order.getId()));
                    return "order-details";
                })
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + id));
    }
}
