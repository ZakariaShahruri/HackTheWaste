import { useEffect, useRef } from "react";
import { useMap } from "react-leaflet";
import * as L from "leaflet";
import type { PredictPoint } from "../../api/types";
import { toHeatGrid, type HeatGrid } from "../../lib/heatmap";

/**
 * Draws the forecast grid as a tiny image (one pixel per grid point) stretched
 * over its area. The browser's smoothing blends the cells, and colours stay on
 * an absolute scale, so a quiet hour looks quiet.
 */
class GridHeatLayer extends L.Layer {
  private canvas: HTMLCanvasElement | null = null;
  private readonly image = document.createElement("canvas");
  private grid: HeatGrid | null = null;

  setGrid(grid: HeatGrid | null) {
    this.grid = grid;
    if (grid) {
      this.image.width = grid.width;
      this.image.height = grid.height;
      this.image.getContext("2d")?.putImageData(new ImageData(grid.pixels, grid.width, grid.height), 0, 0);
    }
    this.draw();
  }

  onAdd(map: L.Map): this {
    this.canvas = L.DomUtil.create("canvas", "heat-canvas", map.getPane("overlayPane"));
    map.on("moveend zoomend resize", this.draw, this);
    this.draw();
    return this;
  }

  onRemove(map: L.Map): this {
    map.off("moveend zoomend resize", this.draw, this);
    this.canvas?.remove();
    this.canvas = null;
    return this;
  }

  private draw() {
    const map = this._map as L.Map | undefined;
    if (!this.canvas || !map) return;

    const size = map.getSize();
    this.canvas.width = size.x;
    this.canvas.height = size.y;
    L.DomUtil.setPosition(this.canvas, map.containerPointToLayerPoint([0, 0]));

    const ctx = this.canvas.getContext("2d");
    if (!ctx) return;
    ctx.clearRect(0, 0, size.x, size.y);
    if (!this.grid) return;

    const { north, south, west, east } = this.grid.bounds;
    const nw = map.latLngToContainerPoint([north, west]);
    const se = map.latLngToContainerPoint([south, east]);
    ctx.imageSmoothingEnabled = true;
    ctx.imageSmoothingQuality = "high";
    ctx.drawImage(this.image, nw.x, nw.y, se.x - nw.x, se.y - nw.y);
  }
}

export function PredictiveHeatmapLayer({ points }: { points: PredictPoint[] }) {
  const map = useMap();
  const layerRef = useRef<GridHeatLayer | null>(null);

  useEffect(() => {
    const layer = new GridHeatLayer().addTo(map);
    layerRef.current = layer;
    return () => {
      map.removeLayer(layer);
      layerRef.current = null;
    };
  }, [map]);

  useEffect(() => {
    layerRef.current?.setGrid(toHeatGrid(points));
  }, [points]);

  return null;
}
