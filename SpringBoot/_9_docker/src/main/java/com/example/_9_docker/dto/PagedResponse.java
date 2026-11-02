package com.example._9_docker.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter 
@Setter 
@AllArgsConstructor 
@NoArgsConstructor 
public class PagedResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private Long totalElements;
    private int totalPages;
    private boolean last;

    public static <E, T> PagedResponse<T> from(Page<E> page, List<T> content){
        return new PagedResponse<>(
                content, 
                page.getNumber(), 
                page.getSize(), 
                page.getTotalElements(), 
                page.getTotalPages(), 
                page.isLast());
    }
}