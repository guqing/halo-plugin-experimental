package xyz.guqing.plugin.potatoes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import xyz.guqing.plugin.potatoes.entity.Potato;
import xyz.guqing.plugin.potatoes.reposiory.PotatoRepository;

/**
 * @author guqing
 * @since 2021-11-09
 */
@Service
public class PotatoService {

    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private PotatoRepository potatoRepository;

    public void create() {
        Potato potato = new Potato();
        potato.setId(1);
        potato.setName("zhangsan的番茄");
        potatoRepository.save(potato);
//        potatoRepository.findAll();
//        userRepository.findAll();
//        personRepository.findAll();

    }
}
