package xyz.guqing.plugin.potatoes.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

/**
 * @author guqing
 * @since 2021-11-08
 */
@Data
@Entity(name = "Potato")
@Table(name = "plugin_potato")
public class Potato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "custom-id")
    @GenericGenerator(name = "custom-id", strategy = "run.halo.app.model.entity.support"
        + ".CustomIdGenerator")
    private Integer id;

    @Column(name = "name", nullable = false)
    private String name;
}
