package edu.ucsb.cs156.frontiers.entities;

import jakarta.persistence.*;
import lombok.*;


@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Entity(name = "admins")
public class Admin {
  @Id
  private String email;
} 