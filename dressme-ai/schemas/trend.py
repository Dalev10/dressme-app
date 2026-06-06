from pydantic import BaseModel, Field


class TrendScoreRequest(BaseModel):
    outfit_embeddings: list[list[float]] = Field(
        ...,
        min_length=1,
        description="Lista de embeddings de las prendas del outfit (1536 dims c/u)",
    )


class TrendScoreResponse(BaseModel):
    trend_score: float
    applies: bool
    dataset_images: int


class TrendScoreBatchRequest(BaseModel):
    """
    Payload para POST /internal/ai/trend/score/batch.

    outfits → Lista de outfits, cada uno con su propia lista de embeddings de prendas.
              El índice de respuesta corresponde al índice de entrada.
    """
    outfits: list[list[list[float]]] = Field(
        ...,
        min_length=1,
        description="Lista de outfits; cada outfit es una lista de embeddings de prendas (1536 dims c/u)",
    )


class TrendScoreBatchResponse(BaseModel):
    """
    Respuesta de POST /internal/ai/trend/score/batch.

    scores → Un TrendScoreResponse por cada outfit en el mismo orden del request.
    """
    scores: list[TrendScoreResponse]