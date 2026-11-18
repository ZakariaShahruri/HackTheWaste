package be.leuven.leuvengo.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "faculty")
public class Faculty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true, length = 10)
    private String shortCode;
    private String color;
    private String emoji;
    private Integer members;
    private Integer points;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "territory_geojson")
    private String territoryGeoJson;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }
    public Integer getMembers() { return members; }
    public void setMembers(Integer members) { this.members = members; }
    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }
    public String getTerritoryGeoJson() { return territoryGeoJson; }
    public void setTerritoryGeoJson(String territoryGeoJson) { this.territoryGeoJson = territoryGeoJson; }
}
