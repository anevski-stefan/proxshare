package com.example.backend.repository;

import com.example.backend.domain.File;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<File, UUID> {

}
