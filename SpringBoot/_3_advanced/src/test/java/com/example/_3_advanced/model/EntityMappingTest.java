package com.example._3_advanced.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
@DisplayName("Entity relationship mapping")
class EntityMappingTest {

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("persists a Book linked to Author, Publisher and Categories and reloads all relationships")
    void persistsAndReloadsRelationships() {
        Author author = Author.builder().firstName("Robert").lastName("Martin").build();
        Publisher publisher = Publisher.builder().name("Prentice Hall").country("USA").build();
        Category clean = Category.builder().name("Clean Code").build();
        Category software = Category.builder().name("Software").build();

        em.persist(author);
        em.persist(publisher);
        em.persist(clean);
        em.persist(software);

        Book book = Book.builder()
                .title("Clean Code")
                .isbn("9780132350884")
                .publicationYear(2008)
                .price(new BigDecimal("42.50"))
                .author(author)
                .publisher(publisher)
                .build();
        book.addCategory(clean);
        book.addCategory(software);

        Book saved = em.persistFlushFind(book);
        em.clear(); // detach everything so the reload actually hits the database

        Book reloaded = em.find(Book.class, saved.getId());
        assertThat(reloaded.getAuthor().getLastName()).isEqualTo("Martin");
        assertThat(reloaded.getPublisher().getName()).isEqualTo("Prentice Hall");
        assertThat(reloaded.getCategories())
                .extracting(Category::getName)
                .containsExactlyInAnyOrder("Clean Code", "Software");

        Author reloadedAuthor = em.find(Author.class, author.getId());
        assertThat(reloadedAuthor.getBooks()).hasSize(1);
    }
}
