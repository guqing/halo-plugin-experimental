package xyz.guqing.plugin.potatoes.reposiory;

import org.springframework.stereotype.Repository;
import run.halo.app.repository.base.BaseRepository;
import xyz.guqing.plugin.potatoes.entity.Person;
import xyz.guqing.plugin.potatoes.entity.Potato;

/**
 * @author guqing
 * @since 2021-11-11
 */
@Repository
public interface PersonRepository extends BaseRepository<Person, Integer> {

}
