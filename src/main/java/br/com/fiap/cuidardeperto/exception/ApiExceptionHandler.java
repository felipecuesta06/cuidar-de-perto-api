package br.com.fiap.cuidardeperto.exception;

import br.com.fiap.cuidardeperto.dto.ErroResponse;
import br.com.fiap.cuidardeperto.dto.ErroResponse.CampoErro;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;


@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<CampoErro> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new CampoErro(e.getField(), e.getDefaultMessage()))
                .toList();
        // Erros de nivel de objeto (ex.: regras @AssertTrue) tambem entram na lista
        List<CampoErro> globais = ex.getBindingResult().getGlobalErrors().stream()
                .map(e -> new CampoErro(e.getObjectName(), e.getDefaultMessage()))
                .toList();
        List<CampoErro> todos = new java.util.ArrayList<>(campos);
        todos.addAll(globais);
        return resposta(HttpStatus.BAD_REQUEST, "Dados invalidos na requisicao", req, todos);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErroResponse> violacao(ConstraintViolationException ex, HttpServletRequest req) {
        List<CampoErro> campos = ex.getConstraintViolations().stream()
                .map(v -> new CampoErro(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return resposta(HttpStatus.BAD_REQUEST, "Dados invalidos na requisicao", req, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST,
                "Corpo da requisicao ausente ou com formato invalido (JSON, datas ou valores de enum)", req, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> tipoInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Valor invalido para o parametro '" + ex.getName() + "'", req,
                List.of(new CampoErro(ex.getName(), "valor invalido")));
    }


    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErroResponse> ordenacaoInvalida(PropertyReferenceException ex, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Campo de ordenacao invalido: '" + ex.getPropertyName() + "'", req,
                List.of(new CampoErro("sort", "campo inexistente: " + ex.getPropertyName())));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroResponse> autenticacao(AuthenticationException ex, HttpServletRequest req) {
        return resposta(HttpStatus.UNAUTHORIZED, "Credenciais invalidas ou conta inativa", req, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> acessoNegado(AccessDeniedException ex, HttpServletRequest req) {
        return resposta(HttpStatus.FORBIDDEN, "Voce nao tem permissao ou vinculo para acessar este recurso", req,
                List.of());
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest req) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ErroResponse> conflito(ConflitoException ex, HttpServletRequest req) {
        return resposta(HttpStatus.CONFLICT, ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> integridade(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Violacao de integridade: {}", ex.getMostSpecificCause().getMessage());
        return resposta(HttpStatus.CONFLICT, "Conflito de dados: registro duplicado ou em uso", req, List.of());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResponse> regraNegocio(RegraNegocioException ex, HttpServletRequest req) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), req, List.of());
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> generica(Exception ex, HttpServletRequest req) {
        if (ex instanceof ErrorResponse springEx) {
            HttpStatusCode codigo = springEx.getStatusCode();
            HttpStatus status = HttpStatus.resolve(codigo.value());
            if (status != null) {
                return resposta(status, status.getReasonPhrase(), req, List.of());
            }
        }
        log.error("Erro inesperado em {}", req.getRequestURI(), ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", req, List.of());
    }

    private ResponseEntity<ErroResponse> resposta(HttpStatus status, String mensagem, HttpServletRequest req,
                                                  List<CampoErro> campos) {
        return ResponseEntity.status(status).body(ErroResponse.de(status, mensagem, req.getRequestURI(), campos));
    }
}
