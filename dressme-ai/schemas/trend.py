from pydantic import BaseModel, Field


class TrendScoreRequest(BaseModel):
    """
    Payload que envía dressme-back al endpoint POST /internal/ai/trend/score.

    outfit_embeddings → Lista de vectores de 1536 dims, uno por prenda del outfit.
                        El outfit_vector final es el promedio de estos vectores.
                        Mínimo 1 prenda (al menos una debe existir).

    Nota de diseño: enviamos los embeddings en lugar de calcularlos aquí porque
    en el futuro cada prenda ya tendrá su propio embedding precalculado y
    almacenado en tbl_clothing_ai_profile (o similar). Reutilizamos ese embedding
    en lugar de llamar a Gemini de nuevo.
    """
    outfit_embeddings: list[list[float]] = Field(
        ...,
        min_length=1,
        description="Lista de embeddings de las prendas del outfit (1536 dims c/u)",
    )


class TrendScoreResponse(BaseModel):
    """
    Respuesta del cálculo de trend score.

    trend_score     → Similitud coseno entre el outfit_vector y el avg_vector
                      del dataset. Valor en [0, 1].
                      1.0 = el outfit es perfectamente representativo de la
                      moda actual del dataset.
                      0.0 = el outfit no tiene nada que ver con las tendencias.

    applies         → False si no hay vector de referencia en la DB (dataset no
                      procesado aún). En ese caso dressme-back debe redistribuir
                      el peso del trend al resto de componentes del score engine,
                      igual que hace con el color score.

    dataset_images  → Cuántas imágenes se usaron para calcular el avg_vector.
                      Útil para logs y para mostrar confianza del score.
    """
    trend_score: float
    applies: bool
    dataset_images: int