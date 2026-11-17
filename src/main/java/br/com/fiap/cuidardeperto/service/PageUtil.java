package br.com.fiap.cuidardeperto.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

public final class PageUtil {

    private PageUtil() {
    }


    public static <T> Page<T> paginar(List<T> lista, Pageable pageable) {
        int inicio = (int) Math.min(pageable.getOffset(), lista.size());
        int fim = Math.min(inicio + pageable.getPageSize(), lista.size());
        return new PageImpl<>(lista.subList(inicio, fim), pageable, lista.size());
    }
}
