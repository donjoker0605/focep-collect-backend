package org.example.collectfocep.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.collectfocep.dto.NotificationDTO;
import org.example.collectfocep.entities.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Map;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(source = "type", target = "type", qualifiedByName = "enumToString")
    @Mapping(source = "metadata", target = "metadata", qualifiedByName = "jsonToMap")
    NotificationDTO toDTO(Notification notification);

    @Mapping(source = "type", target = "type", qualifiedByName = "stringToEnum")
    @Mapping(source = "metadata", target = "metadata", qualifiedByName = "mapToJson")
    Notification toEntity(NotificationDTO dto);

    @Named("enumToString")
    default String enumToString(Notification.NotificationType type) {
        return type != null ? type.name() : null;
    }

    @Named("stringToEnum")
    default Notification.NotificationType stringToEnum(String type) {
        return type != null ? Notification.NotificationType.valueOf(type) : null;
    }

    @Named("jsonToMap")
    default Map<String, Object> jsonToMap(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Named("mapToJson")
    default String mapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}