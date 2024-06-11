package clash.manage.dao;

import clash.manage.model.ClashProxyGroups;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClashProxyGroupsMapper {

    List<ClashProxyGroups> list();
}