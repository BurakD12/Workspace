package com.example._6_security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example._6_security.dto.PublisherRequestDto;
import com.example._6_security.dto.PublisherResponseDto;
import com.example._6_security.exception.DuplicateResourceException;
import com.example._6_security.exception.RelationshipConflictException;
import com.example._6_security.exception.ResourceNotFoundException;
import com.example._6_security.mapper.PublisherMapperImpl;
import com.example._6_security.model.Publisher;
import com.example._6_security.repository.BookRepository;
import com.example._6_security.repository.PublisherRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublisherServiceImpl")
class PublisherServiceImplTest {

    @Mock private PublisherRepository publisherRepository;
    @Mock private BookRepository bookRepository;
    private PublisherServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PublisherServiceImpl(publisherRepository, bookRepository, new PublisherMapperImpl());
    }

    @Test
    @DisplayName("createPublisher saves and returns the mapped response")
    void createPublisher() {
        PublisherRequestDto req = new PublisherRequestDto("Prentice Hall", "USA");
        given(publisherRepository.existsByNameIgnoreCase("Prentice Hall")).willReturn(false);
        given(publisherRepository.save(any(Publisher.class))).willAnswer(inv -> {
            Publisher p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        PublisherResponseDto res = service.createPublisher(req);

        assertThat(res.getId()).isEqualTo(1L);
        assertThat(res.getName()).isEqualTo("Prentice Hall");
        assertThat(res.getCountry()).isEqualTo("USA");
    }

    @Test
    @DisplayName("createPublisher throws DuplicateResourceException for duplicate name")
    void createPublisherDuplicate() {
        given(publisherRepository.existsByNameIgnoreCase("Prentice Hall")).willReturn(true);

        assertThatThrownBy(() -> service.createPublisher(new PublisherRequestDto("Prentice Hall", "USA")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("name");
    }

    @Test
    @DisplayName("getPublisherById throws ResourceNotFoundException when missing")
    void getPublisherByIdMissing() {
        given(publisherRepository.findById(9999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPublisherById(9999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("9999");
    }

    @Test
    @DisplayName("deletePublisher throws RelationshipConflictException when it still has books")
    void deletePublisherWithBooks() {
        Publisher publisher = Publisher.builder().id(1L).name("X").country("USA").build();
        given(publisherRepository.findById(1L)).willReturn(Optional.of(publisher));
        given(bookRepository.existsByPublisherId(1L)).willReturn(true);

        assertThatThrownBy(() -> service.deletePublisher(1L))
                .isInstanceOf(RelationshipConflictException.class);
        verify(publisherRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deletePublisher deletes when publisher has no books")
    void deletePublisherWithoutBooks() {
        Publisher publisher = Publisher.builder().id(1L).name("X").country("USA").build();
        given(publisherRepository.findById(1L)).willReturn(Optional.of(publisher));
        given(bookRepository.existsByPublisherId(1L)).willReturn(false);

        service.deletePublisher(1L);

        verify(publisherRepository).delete(publisher);
    }
}
