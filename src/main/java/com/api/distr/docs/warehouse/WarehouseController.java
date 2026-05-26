package com.api.distr.docs.warehouse;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for warehouse management.
 *
 * GET    /api/warehouse          — active warehouses (for map / route planning)
 * GET    /api/warehouse/all      — all warehouses including inactive (for admin screen)
 * POST   /api/warehouse          — create new warehouse
 * PUT    /api/warehouse/{id}     — update warehouse details
 * DELETE /api/warehouse/{id}     — soft-delete (sets active = false)
 */
@RestController
@RequestMapping("/api/warehouse")
public class WarehouseController {

    @Autowired
    private WarehouseService warehouseService;

    // ── READ ──────────────────────────────────────────────────────────────────

    @GetMapping
    public List<WarehouseDTO> getActiveWarehouses() {
        return warehouseService.getActiveWarehouses();
    }

    @GetMapping("/all")
    public List<WarehouseDTO> getAllWarehouses() {
        return warehouseService.getAllWarehouses();
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<?> createWarehouse(@RequestBody WarehouseDTO dto) {
        try {
            WarehouseDTO created = warehouseService.createWarehouse(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create warehouse: " + e.getMessage());
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    public ResponseEntity<?> updateWarehouse(@PathVariable Long id,
                                             @RequestBody WarehouseDTO dto) {
        try {
            warehouseService.updateWarehouse(id, dto);
            return ResponseEntity.ok("Warehouse updated");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update warehouse: " + e.getMessage());
        }
    }

    // ── DELETE (soft) ─────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deactivateWarehouse(@PathVariable Long id) {
        try {
            warehouseService.deactivateWarehouse(id);
            return ResponseEntity.ok("Warehouse deactivated");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to deactivate warehouse: " + e.getMessage());
        }
    }
}
