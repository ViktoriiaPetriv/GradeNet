package org.bachelor.userservice.repository;

import org.bachelor.userservice.model.entity.BookNumber;
import org.springframework.data.jpa.domain.Specification;

public class BookNumberSpecification {

    private BookNumberSpecification() {}

    public static Specification<BookNumber> numberContains(String number) {
        return (root, query, cb) ->
                number == null || number.isBlank() ? null
                        : cb.like(cb.lower(root.get("number")), "%" + number.toLowerCase() + "%");
    }
}
