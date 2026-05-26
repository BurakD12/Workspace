package com.library.dao;

import com.library.exception.DatabaseException;
import com.library.model.Member;

public interface MemberDAO extends GenericDAO<Member>{
    Member findByEmail(String email) throws DatabaseException;
    void update(Member member) throws DatabaseException;
}
