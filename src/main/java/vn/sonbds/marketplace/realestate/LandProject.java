package vn.sonbds.marketplace.realestate;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Entity
@Table(name = "land_projects")
public class LandProject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long ownerId;
    @NotBlank @Size(max = 180)
    private String name;
    @NotBlank @Size(max = 300)
    private String address;
    @NotBlank
    private String description;
    @PositiveOrZero
    private Integer plotCount;
    @Enumerated(EnumType.STRING)
    private Visibility visibility = Visibility.PUBLIC;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    protected LandProject() {
    }

    public LandProject(Long ownerId, String name, String address, String description, Integer plotCount,
                       Visibility visibility) {
        this.ownerId = ownerId;
        update(name, address, description, plotCount, visibility);
    }

    public void update(String name, String address, String description, Integer plotCount, Visibility visibility) {
        this.name = name;
        this.address = address;
        this.description = description;
        this.plotCount = plotCount;
        this.visibility = visibility;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getOwnerId() { return ownerId; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getDescription() { return description; }
    public Integer getPlotCount() { return plotCount; }
    public Visibility getVisibility() { return visibility; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
