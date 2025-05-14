package edu.ucsb.cs156.frontiers.entities;
import jakarta.persistence.*;
import lombok.*;

/**
 * This is a JPA entity that represents an Instructor.
 */

@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Entity(name = "instructors")
public class Instructor {
    @Id
    private String email;
}
