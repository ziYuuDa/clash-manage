package clash.manage.dao;

import clash.manage.model.ClashRules;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ClashRulesMapper {

    List<ClashRules> list(@Param("verList") Collection<Integer> verList);
}