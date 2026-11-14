package com.pfc.thindesk.dashboard;

import com.mongodb.MongoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = DashboardController.class)
public class DashboardExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(DashboardExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<DashboardResposta.Erro> filtro(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new DashboardResposta.Erro("FILTRO_INVALIDO", exception.getMessage()));
    }

    @ExceptionHandler({DataAccessException.class, MongoException.class})
    public ResponseEntity<DashboardResposta.Erro> banco(Exception exception) {
        log.error("Falha ao consultar dados do dashboard", exception);
        return ResponseEntity.status(503).body(new DashboardResposta.Erro("DADOS_INDISPONIVEIS", "Não foi possível consultar os dados. Tente novamente."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<DashboardResposta.Erro> inesperado(Exception exception) {
        log.error("Falha inesperada no dashboard", exception);
        return ResponseEntity.internalServerError().body(new DashboardResposta.Erro("ERRO_INTERNO", "Não foi possível carregar o dashboard."));
    }
}
