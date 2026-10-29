package com.eventhive.app.dto.request;

import com.eventhive.app.enums.PermisoEvento;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonDeserialize(using = PermisosOperadorRequest.Deserializer.class)
public class PermisosOperadorRequest {

    @JsonAlias({"permisosEvento", "permisos"})
    private Set<PermisoEvento> permisos = new HashSet<>();

    public Set<PermisoEvento> getPermisosEvento() {
        return permisos;
    }

    public void setPermisosEvento(Set<PermisoEvento> permisosEvento) {
        this.permisos = permisosEvento;
    }

    public static class Deserializer extends JsonDeserializer<PermisosOperadorRequest> {
        @Override
        public PermisosOperadorRequest deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            PermisosOperadorRequest request = new PermisosOperadorRequest();
            Set<PermisoEvento> set = new HashSet<>();

            JsonNode node = p.getCodec().readTree(p);
            if (node == null || node.isNull()) {
                request.setPermisos(set);
                return request;
            }

            if (node.isArray()) {
                // Frontend envía array plano: ["CREAR_EVENTO", "CHECK_IN"]
                for (JsonNode item : node) {
                    PermisoEvento perm = PermisoEvento.fromString(item.asText(null));
                    if (perm != null) {
                        set.add(perm);
                    }
                }
            } else if (node.isObject()) {
                // Frontend envía objeto: { permisos: [...] } o { permisosEvento: [...] }
                JsonNode arrayNode = node.get("permisos");
                if (arrayNode == null || arrayNode.isNull()) {
                    arrayNode = node.get("permisosEvento");
                }
                if (arrayNode != null && arrayNode.isArray()) {
                    for (JsonNode item : arrayNode) {
                        PermisoEvento perm = PermisoEvento.fromString(item.asText(null));
                        if (perm != null) {
                            set.add(perm);
                        }
                    }
                }
            }

            request.setPermisos(set);
            return request;
        }
    }
}
