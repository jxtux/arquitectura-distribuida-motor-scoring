package com.finanscore.motorscoring.infrastructure.security.persistence.repository;

import com.finanscore.motorscoring.infrastructure.security.persistence.entity.UserAccountJpaEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AuthorityJpaRepository extends JpaRepository<UserAccountJpaEntity,Long> {
    @Query(value = "select distinct r.nombre from usuario_rol ur join roles r on r.rol_id=ur.rol_id where ur.usuario_app_id=:userId",
           nativeQuery = true)
    List<String> findRoles(@Param("userId") Long userId);

    @Query(value = "select distinct p.nombre from usuario_rol ur join rol_permiso rp on rp.rol_id=ur.rol_id join permisos p on p.permiso_id=rp.permiso_id where ur.usuario_app_id=:userId",
           nativeQuery = true)
    List<String> findPermissions(@Param("userId") Long userId);

    @Modifying
    @Query(value = "insert into usuario_rol(usuario_app_id,rol_id,assigned_at) select :userId,r.rol_id,CURRENT_TIMESTAMP from roles r where r.nombre=:roleName on conflict (usuario_app_id,rol_id) do nothing",
           nativeQuery = true)
    void assignRole(@Param("userId") Long userId, @Param("roleName") String roleName);
}
