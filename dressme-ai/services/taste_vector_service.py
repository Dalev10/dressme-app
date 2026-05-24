import logging
import numpy as np
from schemas.onboarding import (
    ComputeTasteVectorRequest,
    ComputeTasteVectorResponse,
)

logger = logging.getLogger(__name__)

# ── Pesos por reacción ────────────────────────────────────────────────────────
# Estos valores definen cuánto influye cada tipo de reacción en el vector final.
#
# LIKE    → señal fuerte positiva: "quiero looks así"
# DISLIKE → señal negativa suave: no penalizamos duro para no distorsionar
#            el vector con pocas dislikes
# SKIP    → sin información: el usuario no tuvo opinión, no suma ni resta
#
REACTION_WEIGHTS: dict[str, float] = {
    "LIKE": 1.0,
    "DISLIKE": -0.3,
    "SKIP": 0.0,
}


class TasteVectorService:

    def compute(self, request: ComputeTasteVectorRequest) -> ComputeTasteVectorResponse:
        """
        Calcula el taste vector del usuario a partir de sus selecciones.

        Algoritmo:
          1. Construir una matriz (N × 1536) con los embeddings de las tarjetas.
          2. Construir un vector de pesos (N,) con los valores de REACTION_WEIGHTS.
          3. Calcular el promedio ponderado: sumar (embedding_i × peso_i) para todo i.
          4. Normalizar el resultado a longitud unitaria (norma L2).
             La normalización es crucial: permite comparar el taste_vector del usuario
             con los embeddings de sus prendas usando cosine similarity, que es la
             métrica estándar para espacios de embeddings de OpenAI.

        Caso borde — vector cero:
          Si todos los pesos son SKIP o las dislikes cancelan exactamente a los likes,
          el vector resultante antes de normalizar es el vector cero [0,0,...,0].
          Normalizar el vector cero es una división por cero, así que en ese caso
          devolvemos el vector cero directamente. dressme-back interpretará esto como
          "no hay información suficiente" y podrá pedirle al usuario más selecciones.
        """
        logger.info(
            "TasteVectorService: Calculando vector para usuario %s con %d selecciones",
            request.user_id,
            len(request.selections),
        )

        # ── Paso 1: separar embeddings y pesos ───────────────────────────────
        embeddings: list[list[float]] = []
        weights: list[float] = []
        likes_count = 0
        dislikes_count = 0

        for item in request.selections:
            embeddings.append(item.embedding)
            weight = REACTION_WEIGHTS[item.reaction]
            weights.append(weight)

            if item.reaction == "LIKE":
                likes_count += 1
            elif item.reaction == "DISLIKE":
                dislikes_count += 1

        # ── Paso 2: construir estructuras numpy ───────────────────────────────
        # matrix shape: (N, 1536)  →  cada fila es el embedding de una tarjeta
        # weight_vec shape: (N,)   →  cada valor es el peso de esa tarjeta
        matrix = np.array(embeddings, dtype=np.float32)       # (N, 1536)
        weight_vec = np.array(weights, dtype=np.float32)       # (N,)

        # ── Paso 3: promedio ponderado ────────────────────────────────────────
        # weight_vec[:, np.newaxis] expande (N,) a (N, 1) para poder
        # multiplicar elemento a elemento contra matrix (N, 1536)
        weighted_matrix = matrix * weight_vec[:, np.newaxis]   # (N, 1536)
        result_vector = weighted_matrix.sum(axis=0)            # (1536,)

        # ── Paso 4: normalización L2 ──────────────────────────────────────────
        norm = float(np.linalg.norm(result_vector))

        if norm > 1e-9:  # umbral para evitar división por cero con floats
            result_vector = result_vector / norm
            logger.info(
                "TasteVectorService: Vector normalizado. Norma original=%.4f, likes=%d, dislikes=%d",
                norm, likes_count, dislikes_count,
            )
        else:
            logger.warning(
                "TasteVectorService: Vector resultante es cero para usuario %s. "
                "Posibles causas: todas las reacciones son SKIP, o likes y dislikes "
                "se cancelan perfectamente. Devolviendo vector cero.",
                request.user_id,
            )

        return ComputeTasteVectorResponse(
            user_id=request.user_id,
            taste_vector=result_vector.tolist(),
            cards_used=len(request.selections),
            likes_count=likes_count,
            dislikes_count=dislikes_count,
        )