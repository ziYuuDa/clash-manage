package clash.manage.dao;

import clash.manage.model.ClashBasic;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClashBasicMapper {

    List<ClashBasic> list(String parentCode);
}