package com.forum.server.service;

import com.forum.pojo.vo.StatVO;

public interface AdminService {

    void assertAdmin();

    StatVO getStats();
}
